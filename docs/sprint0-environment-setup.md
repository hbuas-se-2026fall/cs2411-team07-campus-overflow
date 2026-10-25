\# Sprint0 本地开发环境部署记录

\## 部署概述

完成CampusOverflow项目本地开发环境搭建，成功启动数据库、缓存服务，后端代码编译通过，前端依赖安装完成。



\## 部署步骤与结果

1\. 通过docker-compose启动MySQL与Redis容器，两个容器状态均为healthy，服务正常就绪。

2\. 后端执行命令：`mvn clean compile -DskipTests`，编译结果 BUILD SUCCESS。

3\. 前端执行命令：`npm install`，前端依赖包安装完成。



\## 部署截图

!\[docker容器状态](docs/screenshots/docker-ps.png)

!\[后端编译结果](docs/screenshots/mvn-build.png)

!\[前端npm安装](docs/screenshots/npm-install.png)



