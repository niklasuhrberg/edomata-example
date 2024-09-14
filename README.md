## Edomata example with http4s server


## Run
Start postgres:
``` sh
docker-compose up
```

In a different shell:

``` sh
sbt catsEffectJVM/run
```

The service has three endpoints. One for metadata retrieval based on id, one for creating a metadata entitiy and one 
for adding a metadata item to an already existing metadata entity.

To create a metadata entity:

`curl -v -X POST localhost:8080/metadata -d "@./samplerequests/metadata.json"`

The `metadataId` must be new. 

To add a metadata entity point to another as its parent:
`curl -v -X POST localhost:8080/metadata -d "@./samplerequests/metadatap.json"`

The `parent` must be the `metadataId` of an already existing entity

To add a metadata item to an already existing metadata entity: 

`curl -v -X POST localhost:8080/metadata/d857ccd3-1778-49c4-a0aa-b99c2fe8e4a7/items -d "@./samplerequests/item.json"`

Where the UUID in the path is the metadata id of an existing entity