.PHONY: start stop restart status logs seed

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
