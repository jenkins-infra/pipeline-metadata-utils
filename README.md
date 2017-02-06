## Pipeline (and Describable) Metadata Utils

This is a Java library of utility classes for extracting information about 
Pipeline steps or other `Describable` classes from Jenkins for use in 
generating documentation or other reports on all possible steps, 
`Describable`s, etc, as provided by any and all plugins.

### Origin of code

`MockExtensionLists`, `MockJenkins`, and `HyperLocalPluginManager` originated
in the [pipeline-steps-doc-generator](https://github.com/jenkins-infra/pipeline-steps-doc-generator)
tool, which is used for generating AsciiDoc for all Pipeline steps.
