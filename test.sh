mvn clean cobertura:cobertura -Dcobertura.aggregate=true && mvn surefire-report:report -DskipTests=true -Daggregate=true