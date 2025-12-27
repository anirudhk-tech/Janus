.PHONY: run test
run:
	@set -a && . ./.env && set +a && ./mvnw spring-boot:run

test:
	@./mvnw test