---
name: neil
description: Use babashka/neil to manage deps.edn dependencies, add test runners, build aliases, and scaffold projects. Activate when user wants to add libs, search Clojars, upgrade deps, add kaocha/nrepl/build setup, or manage project structure.
---

## neil CLI — Common Commands

### Add project tooling (most frequently used)

```bash
# Add tools.build setup (build.clj + :build alias)
neil add build

# Add kaocha test runner (:kaocha alias)
neil add kaocha

# Add nREPL server (:nrepl alias)
neil add nrepl

# Add cognitect test runner (:test alias)
neil add test
```

### Dependency management

```bash
# Add a library to :deps
neil dep add --lib <lib>                        # e.g. metosin/malli
neil dep add --lib <lib> --version <version>    # pin to specific version
neil dep add --lib <lib> --latest-sha           # use latest GitHub SHA

# Search Clojars
neil dep search <term>

# Upgrade all deps
neil dep upgrade

# List available versions (Clojars only)
neil dep versions --lib <lib>
```

### Project scaffolding

```bash
neil new <template> <project-name>   # e.g. neil new scratch my-project
```

### License

```bash
neil license list              # list common licenses
neil license add --license mit # write LICENSE file
```

## Notes

- All subcommands support `--deps-file` to target a non-default deps.edn.
- All subcommands support `--alias` to override the generated alias name.
- `neil add build` generates a `build.clj` and `:build` alias wired to `tools.build`.
- `neil add kaocha` assumes kaocha is the preferred test runner over cognitect runner.
- `neil add nrepl` adds a `:nrepl` alias so you can start a REPL server with `clj -M:nrepl`.
