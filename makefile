compile :
	javac -d ./ src/anonpds/TaskMistress/*.java

jar : compile
	jar cfe TaskMistress.jar anonpds/TaskMistress/TaskMistress anonpds/TaskMistress/*.class

clean :
	$(RM) -r anonpds src/anonpds/TaskMistress/*.class TaskMistress.jar
