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
# cronjob-agent script.
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

# Only set CRONJOB_HOME if not already set
test ".$CRONJOB_HOME"   = . && CRONJOB_HOME=`cd "$DIRNAME/.." >/dev/null; pwd`
test ".$CRONJOB_LIB"    = . && CRONJOB_LIB="$CRONJOB_HOME/lib"
test ".$CRONJOB_MAIN"   = . && CRONJOB_MAIN=org.jcronjob.agent.AgentBootstrap
test ".$CRONJOB_JAR"    = . && CRONJOB_JAR="$CRONJOB_LIB/cronjob-agent-1.0-SNAPSHOT.jar"
test ".$CLASSPATH"     != . && CLASSPATH="${CLASSPATH}:"
test ".$CRONJOB_USER"   = . && CRONJOB_USER=root
test ".$CRONJOB_OUT"    = . && CRONJOB_OUT="$CRONJOB_HOME/logs/cronjob.out"
test ".$CRONJOB_TMPDIR" = . && CRONJOB_TMPDIR="$CRONJOB_HOME/temp"
test ".$CRONJOB_PID"    = . && CRONJOB_PID="$CRONJOB_HOME/cronjob.pid"

if [ -z "$JSVC" ]; then
    CLASSPATH="$CLASSPATH$CRONJOB_JAR"
    JSVC="$CRONJOB_HOME/lib/jsvc/jsvc"
    if [ ! -x "$JSVC" ]; then
        JSVC="$CRONJOB_HOME/lib/jsvc/jsvc"
    fi
fi

ls $CRONJOB_LIB/jsvc -l|awk 'NR>1{print $1,$NF}'|while read x y;do [ $x = "-rw-r--r--." ]&&chmod 777 $CRONJOB_LIB/jsvc/$y;done

have_tty=0
if [ "`tty`" != "not a tty" ]; then
    have_tty=1
fi

if [ $have_tty -eq 1 ]; then

     echo  "  Using CRONJOB_HOME:   $CRONJOB_HOME   "
     echo  "  Using CRONJOB_TMPDIR: $CRONJOB_TMPDIR "
     echo  "  Using CRONJOB_MAIN:   $CRONJOB_MAIN   "
     echo  "  Using CRONJOB_OUT:    $CRONJOB_OUT    "
     echo  "  Using CLASSPATH:      $CLASSPATH      "
     [ -d "$CRONJOB_PID" ] &&  echo  "  Using CRONJOB_PID:    ${CRONJOB_PID}  "

fi

case "$1" in

    start)
        GETOPT_ARGS=`getopt -o P:p: -al port:,password: -- "$@"`
        eval set -- "$GETOPT_ARGS"
        while [ -n "$1" ]
        do
            case "$1" in
                -P|--port)
                    CRONJOB_PORT=$2;
                    shift 2;;
                -p|--password)
                    CRONJOB_PASSWORD=$2;
                    shift 2;;
                --) break ;;
                *)
                    echo "usage {-P\${port}|-p\${pasword}}"
                 break ;;
            esac
        done

        if [ -z "$CRONJOB_PORT" ];then
            echo "  cronjob port must be input.."
            exit 1;
        elif [ $CRONJOB_PORT -lt 0 ] || [ $CRONJOB_PORT -gt 65535 ];then
            echo "port error,muse be between 0 and 65535!"

        fi

        [ -n "$CRONJOB_PASSWORD" ] && CRONJOB_PASSWORD="-pass $CRONJOB_PASSWORD";

        "$JSVC" $JSVC_OPTS \
        -java-home "$JAVA_HOME" \
        -user $CRONJOB_USER \
        -pidfile "$CRONJOB_PID" \
        -outfile "$CRONJOB_OUT" \
        -errfile "&1" \
        -classpath "$CLASSPATH" \
        -Dcronjob.home="$CRONJOB_HOME" \
        -Djava.io.tmpdir="$CRONJOB_TMPDIR" \
        $CRONJOB_MAIN \
        -port $CRONJOB_PORT \
        $CRONJOB_PASSWORD

        echo "  cronjob Starting..."
        sleep 1;
        exit $?
    ;;

    stop)
      "$JSVC" $JSVC_OPTS \
      -stop \
      -pidfile "$CRONJOB_PID" \
      -classpath "$CLASSPATH" \
      -Dcronjob.home="$CRONJOB_HOME" \
      -Djava.io.tmpdir="$CRONJOB_TMPDIR" \
      $CRONJOB_MAIN
      exit $?
    ;;

    *)
      echo "Unknown command: \`$1'"
      echo "Usage: $PROGRAM ( commands ... )"
      echo "commands:"
      echo "  start             Start Cronjob"
      echo "  stop              Stop Cronjob"
      echo "                    are you running?"
      exit 1
    ;;
    esac
exit 0;