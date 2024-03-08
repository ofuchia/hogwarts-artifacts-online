package edu.tcu.cs.hogwartsartifactsonline.artifact;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional //each method has own transaction
public class ArtifactService {

    private final ArtifactRepository artifactRepository;


    public ArtifactService(ArtifactRepository artifactRepository) {
        this.artifactRepository = artifactRepository;
    }

    public Artifact findById(String artifactId){
        return this.artifactRepository.findById(artifactId) //return if found
                .orElseThrow(() -> new ArtifactNotFoundException(artifactId)); //throw exception if not found
    }
}
