package edu.tcu.cs.hogwartsartifactsonline.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.tcu.cs.hogwartsartifactsonline.artifact.dto.ArtifactDto;
import edu.tcu.cs.hogwartsartifactsonline.artifact.utils.IdWorker;
import edu.tcu.cs.hogwartsartifactsonline.client.ai.chat.ChatClient;
import edu.tcu.cs.hogwartsartifactsonline.client.ai.chat.dto.ChatRequest;
import edu.tcu.cs.hogwartsartifactsonline.client.ai.chat.dto.ChatResponse;
import edu.tcu.cs.hogwartsartifactsonline.client.ai.chat.dto.Message;
import edu.tcu.cs.hogwartsartifactsonline.system.exception.ObjectNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@Transactional //each method has own transaction
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final IdWorker idWorker;
    private final ChatClient chatClient;

    //Constructor
    public ArtifactService(ArtifactRepository artifactRepository, IdWorker idWorker, ChatClient chatClient) {
        this.artifactRepository = artifactRepository;
        this.idWorker = idWorker;
        this.chatClient = chatClient;
    }

    public Artifact findById(String artifactId){
        return this.artifactRepository.findById(artifactId) //return if found
                .orElseThrow(() -> new ObjectNotFoundException("artifact", artifactId)); //throw exception if not found
    }


    //returns an empty list if no artifact is present. due to the artifact List declaration, we avoid null pointer error
    public List<Artifact> findAll(){
        return this.artifactRepository.findAll();
    }

    public Artifact save(Artifact newArtifact){
        newArtifact.setId(idWorker.nextId() + "");
        return this.artifactRepository.save(newArtifact);

    }

    public Artifact update(String artifactId, Artifact update){
            return this.artifactRepository.findById(artifactId)
                .map(oldArtifact ->{ //if found, run the below
                    oldArtifact.setName(update.getName());
                    oldArtifact.setDescription((update.getDescription()));
                    oldArtifact.setImageUrl(update.getImageUrl());
                    return this.artifactRepository.save(oldArtifact);
                })
                .orElseThrow(() -> new ObjectNotFoundException("artifact", artifactId)); //if not found, throw exception



    }

    public void delete(String artifactId){
        this.artifactRepository.findById(artifactId)
                .orElseThrow(() -> new ObjectNotFoundException("artifact",artifactId));
        this.artifactRepository.deleteById(artifactId);


    }

    public String summarize(List<ArtifactDto> artifactDtos) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonArray = objectMapper.writeValueAsString(artifactDtos);

        // Prepare the messages for summarizing.
        List<Message> messages = List.of(
                new Message("system", "Your task is to generate a short summary of a given JSON array in at most 100 words. The summary must include the number of artifacts, each artifact's description, and the ownership information. Don't mention that the summary is from a given JSON array."),
                new Message("user", jsonArray)
        );

        ChatRequest chatRequest = new ChatRequest("gpt-4", messages);

        ChatResponse chatResponse = this.chatClient.generate(chatRequest); // Tell chatClient to generate a text summary based on the given chatRequest.

        // Retrieve the AI-generated text and return to the controller.
        return chatResponse.choices().get(0).message().content();
    }

    public Page<Artifact> findAll(Pageable pageable) {
        return this.artifactRepository.findAll(pageable);
    }

    public Page<Artifact> findByCriteria(Map<String, String> searchCriteria, Pageable pageable) {
        Specification<Artifact> spec = Specification.where(null);

        if (StringUtils.hasLength(searchCriteria.get("id"))) {
            spec = spec.and(ArtifactSpecs.hasId(searchCriteria.get("id")));
        }

        if (StringUtils.hasLength(searchCriteria.get("name"))) {
            spec = spec.and(ArtifactSpecs.containsName(searchCriteria.get("name")));
        }

        if (StringUtils.hasLength(searchCriteria.get("description"))) {
            spec = spec.and(ArtifactSpecs.containsDescription(searchCriteria.get("description")));
        }

        if (StringUtils.hasLength(searchCriteria.get("ownerName"))) {
            spec = spec.and(ArtifactSpecs.hasOwnerName(searchCriteria.get("ownerName")));
        }

        return this.artifactRepository.findAll(spec, pageable);
    }


}
