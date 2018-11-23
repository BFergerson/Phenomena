## Phenomena: Contextual source code behavior integration 
Phenomena is designed to support any type of graph-based representation of source code.
Phenomena's base structure uses the omnilingual source code schema provided by [OmniSRC](https://github.com/CodeBrig/OmniSRC).
This schema can be extended with custom definitions and inference rules which are introduced by using a CodeObserver.

Phenomena comes with the following code observers:

### Structure

#### Description
[CodeStructureObserver](https://github.com/CodeBrig/Phenomena/blob/master/src/main/groovy/com/codebrig/phenomena/code/structure/CodeStructureObserver.groovy)
is the base observer which is required to use Phenomena.
This observer creates nodes and edges which contain the structure of the source code in the form of an abstract syntax graph.

#### Observers

| Structure                   | Supported language(s)              |
| --------------------------- | ---------------------------------- |
| Abstract syntax tree        | Go, Java, JavaScript, Python       |
| Semantic roles              | Go, Java, JavaScript, Python       |

### Dependence

#### Description

#### Observers

| Metric                      | Supported language(s)              |
| --------------------------- | ---------------------------------- |
| Identifier access           | Java                               |
| Method call                 | Java                               |

### Metric

#### Description

#### Observers

| Metric                      | Supported language(s)              |
| --------------------------- | ---------------------------------- |
| Cyclomatic complexity       | [Go, Java, JavaScript, Python](https://github.com/CodeBrig/Phenomena/blob/v0.2-alpha/src/main/groovy/com/codebrig/phenomena/code/analysis/metric/CyclomaticComplexity.groovy) |

#### Schema
[OmniSRC_Omnilingual_Base_Structure (v0.3-alpha)](https://github.com/CodeBrig/OmniSRC/blob/v0.3-alpha/src/main/resources/schema/omnilingual/OmniSRC_Omnilingual_Base_Structure.gql)
