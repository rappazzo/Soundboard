#!/bin/bash
export ENVIRONMENT_HOME="."
export LIBDIR="$ENVIRONMENT_HOME/lib"

export JDK="$JAVA_HOME"
export CLASSPATH="$JDK/jre/lib/rt.jar;$JDK/lib/tools.jar;$ENVIRONMENT_HOME/target;$LIBDIR/net.javazoom/jl1.0.jar;$LIBDIR/net.javazoom/mp3spi1.9.4.jar;$LIBDIR/net.javazoom/tritonus_share.jar;$LIBDIR/com.lotus.sametime/sametime.jar;$LIBDIR/javax.servlet/servlet.jar;$LIBDIR/net.sf.freetts/freetts.jar;$LIBDIR/org.igniterealtime.smack_3.1.0/smackx.jar;$LIBDIR/org.igniterealtime.smack_3.1.0/smack.jar"
$JDK/jre/bin/java -server -Xms256m -Xmx256m -XX:NewRatio=3 -XX:SurvivorRatio=5 org.soundboard.server.Server $ENVIRONMENT_HOME/bin/soundboard.properties

