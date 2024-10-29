.PHONY: release-backend, release-cloud

release-backend:
	cd backend && mvn clean package -DskipTests=true
	cp backend/target/backend-*.jar backend/docker/app/application.jar


release-cloud:
	cd cloud && mvn clean package -DSkipTests=true
	cp cloud/order/target/order-*.jar cloud/docker/app/order.jar
	cp cloud/order/target/others-*.jar cloud/docker/app/others.jar
	cp cloud/order/target/product-*.jar cloud/docker/app/product.jar