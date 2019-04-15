## Phenomena: Contextual source code behavior integration

[![Build Status](https://travis-ci.com/CodeBrig/Phenomena.svg?branch=master)](https://travis-ci.com/CodeBrig/Phenomena)

Phenomena is designed to support any type of graph-based representation of source code.
Phenomena's base structure uses the omnilingual source code schema provided by [OmniSRC](https://github.com/CodeBrig/OmniSRC).
This schema can be extended with custom definitions and inference rules which are introduced by using a CodeObserver.

Phenomena currently supports the following types of code observers:

### Structure

#### Description
[CodeStructureObserver](https://github.com/CodeBrig/Phenomena/blob/v0.2-alpha/src/main/groovy/com/codebrig/phenomena/code/structure/CodeStructureObserver.groovy)
is the base observer which is required to use Phenomena.
This observer creates nodes and edges which contain the structure of the source code in the form of an abstract syntax graph.

#### Observers

| Structure                   | Supported language(s)              |
| --------------------------- | ---------------------------------- |
| Abstract syntax tree        | [Go, Java, JavaScript, Php, Python, Ruby](https://github.com/CodeBrig/OmniSRC/blob/v0.3.1-alpha/src/main/resources/schema/omnilingual) |
| Semantic roles              | [Go, Java, JavaScript, Php, Python, Ruby](https://github.com/CodeBrig/OmniSRC/tree/v0.3.1-alpha/src/main/resources/schema/omnilingual) |

### Dependence

#### Description

The dependence observers create edges between program statements and the preceding statements of which they depend on.

#### Observers

| Metric                      | Supported language(s)              |
| --------------------------- | ---------------------------------- |
| Identifier access           | [Java](https://github.com/CodeBrig/Phenomena/blob/v0.2-alpha/src/main/groovy/com/codebrig/phenomena/code/analysis/language/java/dependence/JavaIdentifierAccessObserver.groovy) |
| Method call                 | [Java](https://github.com/CodeBrig/Phenomena/blob/v0.2-alpha/src/main/groovy/com/codebrig/phenomena/code/analysis/language/java/dependence/JavaMethodCallObserver.groovy) |

### Metric

#### Description

The metric observers create attributes on correlating source code nodes with calculated metric data.

#### Observers

| Metric                      | Supported language(s)              |
| --------------------------- | ---------------------------------- |
| Cyclomatic complexity       | [Go, Java, JavaScript, Php, Python, Ruby](https://github.com/CodeBrig/Phenomena/blob/v0.2-alpha/src/main/groovy/com/codebrig/phenomena/code/analysis/metric/CyclomaticComplexityObserver.groovy) |
