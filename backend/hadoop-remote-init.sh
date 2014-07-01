#!/usr/bin/env bash

HADOOP_HOME="/usr/local/hadoop-1.2.1"
MASTER_IP=$1
LOCAL_IP=`curl http://169.254.169.254/latest/meta-data/local-ipv4`
IS_MASTER=false

sudo mkdir /mnt/hadoop

if [ $MASTER_IP == $LOCAL_IP ]; then
  IS_MASTER=true
fi

cat > $HADOOP_HOME/conf/core-site.xml <<EOF
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<!-- Put site-specific property overrides in this file. -->

<configuration>
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/mnt/hadoop</value>
        <description>A base for other temporary directories.</description>
    </property>
    <property>
        <name>fs.default.name</name>
        <value>hdfs://$MASTER_IP:9000</value>
    </property>
</configuration>
EOF

cat > $HADOOP_HOME/conf/hdfs-site.xml <<EOF
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<!-- Put site-specific property overrides in this file. -->

<configuration>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
    <property>
        <name>dfs.permissions.enabled</name>
        <value>false</value>
    </property>
</configuration>
EOF

cat > $HADOOP_HOME/conf/mapred-site.xml <<EOF
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>

<!-- Put site-specific property overrides in this file. -->

<configuration>
    <property>
        <name>mapred.job.tracker</name>
        <value>http://$MASTER_IP:9001</value>
    </property>
</configuration>
EOF

if [ "$IS_MASTER" == "true" ]; then
  [ ! -e /mnt/hadoop/dfs ] && sudo "$HADOOP_HOME"/bin/hadoop namenode -format
  sudo "$HADOOP_HOME"/bin/hadoop-daemon.sh start namenode
  sudo "$HADOOP_HOME"/bin/hadoop-daemon.sh start jobtracker
  sudo "$HADOOP_HOME"/bin/hadoop-daemon.sh start datanode
  sudo "$HADOOP_HOME"/bin/hadoop-daemon.sh start tasktracker
else
  sudo "$HADOOP_HOME"/bin/hadoop-daemon.sh start datanode
  sudo "$HADOOP_HOME"/bin/hadoop-daemon.sh start tasktracker
fi

