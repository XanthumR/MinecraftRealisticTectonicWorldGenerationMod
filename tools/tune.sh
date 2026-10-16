# Earth climate against Köppen with jshell -R-D options passed as arguments: bash tools/tune.sh -R-Drt.pr=10
J="/c/Program Files/Java/jdk-21.0.12/bin/jshell"; CP="build/classes/java/main;build/resources/main"
"$J" -R-Xmx6g "$@" --class-path "$CP" tools/earthclim.jsh > /dev/null && python tools/koppen.py | sed -n 2,5p && python tools/kregion.py
