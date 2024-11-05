.PHONY: release-backend, release-cloud

release-backend:
	cd backend && mvn clean package -DskipTests=true
	mkdir -p backend/docker/app
	cp backend/target/backend-*.jar backend/docker/app/application.jar


release-cloud:
	cd cloud && mvn clean package -DSkipTests=true
	mkdir -p cloud/docker/app
	cp cloud/order/target/order-*.jar cloud/docker/app/order.jar
	cp cloud/others/target/others-*.jar cloud/docker/app/others.jar
	cp cloud/product/target/product-*.jar cloud/docker/app/product.jar