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

# Tag current image before rebuilding (for rollback)
deploy:
	docker compose -f docker-compose.yml exec -T api echo "pre-deploy check" || true
	docker tag hookwatch-api:latest hookwatch-api:previous 2>/dev/null || true
	cd api && mvn package -DskipTests -q
	docker compose build api --no-cache
	docker compose up -d --no-deps api
	@echo "Waiting for health check..."
	@for i in $$(seq 1 30); do \
		if curl -sf http://localhost:8080/api/v1/health > /dev/null 2>&1; then \
			echo "Deploy successful after $${i}s"; \
			exit 0; \
		fi; \
		sleep 1; \
	done; \
	echo "Deploy failed - rolling back"; \
	$(MAKE) rollback

rollback:
	docker tag hookwatch-api:previous hookwatch-api:latest 2>/dev/null || (echo "No previous image to rollback to" && exit 1)
	docker compose up -d --no-deps api
	@echo "Rolled back to previous image"
