.PHONY: up down logs build restart clean deploy rollback postman-export check-route-collisions

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

postman-export:
	@mkdir -p docs/postman
	npx --yes openapi-to-postmanv2 -s http://localhost:8080/api/v1/openapi.json -o docs/postman/hookwatch.postman_collection.json -p
	@echo "Postman collection generated at docs/postman/hookwatch.postman_collection.json"

check-route-collisions:
	python3 scripts/check_route_basename_collisions.py
