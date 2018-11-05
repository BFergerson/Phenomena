## Phenomena: Contextual source code behavior integration 
Phenomena is designed to support any type of graph-based representation of source code. Phenomena's base structure uses the omnilingual source code schema provided by [OmniSRC](https://github.com/CodeBrig/OmniSRC). This schema can be extended with custom definitions and inference rules which are introduced by using a CodeObserver.

Phenomena comes with the following code observers:
- [CodeStructureObserver](https://github.com/CodeBrig/Phenomena/blob/master/src/main/groovy/com/codebrig/phenomena/code/structure/CodeStructureObserver.groovy) is the base observer which is required to use Phenomena. This observer creates nodes and edges which contain the structure of the source code in the form of an abstract syntax graph.

## Schema

Structure: [OmniSRC_Omnilingual_Schema-1.0](https://github.com/CodeBrig/OmniSRC/blob/master/src/main/resources/schema/omnilingual/OmniSRC_Omnilingual_Schema-1.0.gql)
