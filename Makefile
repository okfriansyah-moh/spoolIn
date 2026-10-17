.PHONY: start stop restart status logs seed test-unit test-integration

start:
	@bash ops/bin/start.sh
stop:
	@bash ops/bin/stop.sh
restart:
	@bash ops/bin/restart.sh
status:
	@bash ops/bin/status.sh
logs:
	@bash ops/bin/logs.sh $(SERVICE)
seed:
	@bash ops/bin/seed.sh
test-unit:
	@bash ops/bin/test-unit.sh
test-integration:
	@bash ops/bin/test-integration.sh
