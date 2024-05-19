#!/bin/sh
java -Djava.util.logging.config.file=config/console.cfg -cp ../libs/*:l2jserver.jar:mariadb-java-client-3.1.2 net.sf.l2j.accountmanager.SQLAccountManager
