Required tooling
----------------

Asciidoc: brew install asciidoc

Asciidoctor: sudo gem install asciidoctor


Install Syntax highlighter
-------------------------

sudo easy_install Pygments



Install bootstrap backend
-------------------------

asciidoc --backend install bootstrap-<version>.zip

(file is included in this folder)

(Note: custom Activiti favicon installed here!)



Generate HTML docs
------------------

./generate-html.sh