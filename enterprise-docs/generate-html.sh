cp -f favicon.* $HOME/.asciidoc/backends/bootstrap/images/icons/
asciidoc -b bootstrap -a bsver=3 -a source-highlighter=pygments -a brand="Alfresco Activiti Enterprise User Guide" -a toc2 -a sidebar=left -a jumbotron -a footer=custom *.adoc
