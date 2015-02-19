# App, rendered on server

`boot dev` starts CLJS compilation loop and a REPL server. In separate console
run `boot repl -c` (in this directory, so that it can find `.nrepl-port` file)
and `(phoenix/start!)` there to get server side running.
