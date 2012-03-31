compile :
	javac -d ./ src/anonpds/TaskMistress/*.java

jar : compile
	jar cfe TaskMistress.jar anonpds/TaskMistress/TaskMistress anonpds/TaskMistress/*.class res/*

clean :
	$(RM) -r anonpds src/anonpds/TaskMistress/*.class TaskMistress.jar
