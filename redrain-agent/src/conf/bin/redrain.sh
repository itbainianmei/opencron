#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# -----------------------------------------------------------------------------
# redrain-agent script.
# -----------------------------------------------------------------------------
#
# resolve links - $0 may be a softlink
ARG0="$0"
while [ -h "$ARG0" ]; do
  ls=`ls -ld "$ARG0"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    ARG0="$link"
  else
    ARG0="`dirname $ARG0`/$link"
  fi
done
DIRNAME="`dirname $ARG0`"
PROGRAM="`basename $ARG0`"

if [ -z "$JAVA_HOME" ]; then
    JAVA_BIN="`which java 2>/dev/null || type java 2>&1`"
    test -x "$JAVA_BIN" && JAVA_HOME="`dirname $JAVA_BIN`"
    test ".$JAVA_HOME" != . && JAVA_HOME=`cd "$JAVA_HOME/.." >/dev/null; pwd`
else
    JAVA_BIN="$JAVA_HOME/bin/java"
fi

# Only set REDRAIN_HOME if not already set
test ".$REDRAIN_HOME"   = . && REDRAIN_HOME=`cd "$DIRNAME/.." >/dev/null; pwd`
test ".$REDRAIN_LIB"    = . && REDRAIN_LIB="$REDRAIN_HOME/lib"
test ".$REDRAIN_MAIN"   = . && REDRAIN_MAIN=com.jredrain.agent.AgentBootstrap
test ".$REDRAIN_JAR"    = . && REDRAIN_JAR="$REDRAIN_LIB/redrain-agent-1.0-SNAPSHOT.jar"
test ".$CLASSPATH"     != . && CLASSPATH="${CLASSPATH}:"
test ".$REDRAIN_USER"   = . && REDRAIN_USER=root
test ".$REDRAIN_OUT"    = . && REDRAIN_OUT="$REDRAIN_HOME/logs/redrain.out"
test ".$REDRAIN_TMPDIR" = . && REDRAIN_TMPDIR="$REDRAIN_HOME/temp"
test ".$REDRAIN_PID"    = . && REDRAIN_PID="$REDRAIN_HOME/redrain.pid"

if [ -z "$JSVC" ]; then
    CLASSPATH="$CLASSPATH$REDRAIN_JAR"
    JSVC="$REDRAIN_HOME/lib/jsvc/jsvc"
    if [ ! -x "$JSVC" ]; then
        JSVC="$REDRAIN_HOME/lib/jsvc/jsvc"
    fi
fi

chmod 777 $REDRAIN_LIB/jsvc/*;

have_tty=0
if [ "`tty`" != "not a tty" ]; then
    have_tty=1
fi

if [ $have_tty -eq 1 ]; then

     echo  "  Using REDRAIN_HOME:   $REDRAIN_HOME   "
     echo  "  Using REDRAIN_TMPDIR: $REDRAIN_TMPDIR "
     echo  "  Using REDRAIN_MAIN:   $REDRAIN_MAIN   "
     echo  "  Using REDRAIN_OUT:    $REDRAIN_OUT    "
     echo  "  Using CLASSPATH:      $CLASSPATH      "
     [ -d "$REDRAIN_PID" ] &&  echo  "  Using REDRAIN_PID:    ${REDRAIN_PID}  "

fi

case "$1" in

    start)
        GETOPT_ARGS=`getopt -o P:p: -al port:,password: -- "$@"`
        eval set -- "$GETOPT_ARGS"
        while [ -n "$1" ]
        do
            case "$1" in
                -P|--port)
                    REDRAIN_PORT=$2;
                    shift 2;;
                -p|--password)
                    REDRAIN_PASSWORD=$2;
                    shift 2;;
                --) break ;;
                *)
                    echo "usage {-P\${port}|-p\${pasword}}"
                 break ;;
            esac
        done

        if [ -z "$REDRAIN_PORT" ];then
            echo "  redrain port must be input.."
            exit 1;
        elif [ $REDRAIN_PORT -lt 0 ] || [ $REDRAIN_PORT -gt 65535 ];then
            echo "port error,muse be between 0 and 65535!"

        fi

        [ -n "$REDRAIN_PASSWORD" ] && REDRAIN_PASSWORD="-pass $REDRAIN_PASSWORD";

        "$JSVC" $JSVC_OPTS \
        -java-home "$JAVA_HOME" \
        -user $REDRAIN_USER \
        -pidfile "$REDRAIN_PID" \
        -outfile "$REDRAIN_OUT" \
        -errfile "&1" \
        -classpath "$CLASSPATH" \
        -Dredrain.home="$REDRAIN_HOME" \
        -Djava.io.tmpdir="$REDRAIN_TMPDIR" \
        $REDRAIN_MAIN \
        -port $REDRAIN_PORT \
        $REDRAIN_PASSWORD

        echo "  redrain Starting..."
        sleep 1;
        exit $?
    ;;

    stop)
      "$JSVC" $JSVC_OPTS \
      -stop \
      -pidfile "$REDRAIN_PID" \
      -classpath "$CLASSPATH" \
      -Dredrain.home="$REDRAIN_HOME" \
      -Djava.io.tmpdir="$REDRAIN_TMPDIR" \
      $REDRAIN_MAIN
      exit $?
    ;;

    *)
      echo "Unknown command: \`$1'"
      echo "Usage: $PROGRAM ( commands ... )"
      echo "commands:"
      echo "  start             Start RedRain"
      echo "  stop              Stop RedRain"
      echo "                    are you running?"
      exit 1
    ;;
    esac
exit 0;