.PHONY: up down logs build restart clean deploy rollback

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

build:
	docker compose build

restart:
	docker compose restart

clean:
	docker compose down -v --remove-orphans

deploy:
	@echo "Deploying..."
	docker tag hookwatch-api:latest hookwatch-api:previous 2>/dev/null || true
	cd api && mvn package -DskipTests -q
	docker compose build api --no-cache
	docker compose up -d --no-deps api
	@echo "Waiting for startup..."
	@sleep 15
	@curl -sf http://localhost:8080/api/v1/health && echo " Deploy OK" || (echo " FAILED - rolling back" && docker tag hookwatch-api:previous hookwatch-api:latest && docker compose up -d --no-deps api && exit 1)

rollback:
	@echo "Rolling back to previous image..."
	docker tag hookwatch-api:previous hookwatch-api:latest
	docker compose up -d --no-deps api
	@sleep 15
	@curl -sf http://localhost:8080/api/v1/health && echo " Rollback OK" || echo " Rollback FAILED"
