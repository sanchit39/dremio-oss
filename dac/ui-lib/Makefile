SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

.DEFAULT_GOAL := help

.PHONY: clean prepare storybook help

COMPONENTS = $(shell find components -type f)
ICONS = $(shell find icons -type f)
SRC = $(shell find src -type f)
THEMES = $(shell find themes -type f)

clean: ## Clean all build and install artifacts
	@git clean -dfX

dist: node_modules tsconfig.src.json $(SRC)
	pnpm exec microbundle-crl --css-modules false --format modern,cjs
	pnpm exec tsc --project tsconfig.src.json --emitDeclarationOnly --declaration --declarationMap false --declarationDir dist
	@touch dist

dist-cjs: node_modules .config/.swcrc-cjs tsconfig.components.json $(COMPONENTS)
	pnpm exec swc ./components --config-file .config/.swcrc-cjs --out-dir dist-cjs
	pnpm exec tsc --project tsconfig.components.json --emitDeclarationOnly --declaration --declarationMap false --declarationDir dist-cjs
	@touch dist-cjs

dist-esm: node_modules .config/.swcrc-esm tsconfig.components.json $(COMPONENTS)
	pnpm exec swc ./components --config-file .config/.swcrc-esm --out-dir dist-esm
	pnpm exec tsc --project tsconfig.components.json --emitDeclarationOnly --declaration --declarationMap false --declarationDir dist-esm
	@touch dist-esm

dist-icons: node_modules iconmanifest.json $(ICONS)
	pnpm exec ts-node ./scripts/buildIconSprites.ts

dist-themes: node_modules $(THEMES)
	node scripts/buildThemes.mjs
	@touch dist-themes

iconmanifest.json: node_modules $(ICONS)
	pnpm exec ts-node scripts/writeIconManifest.ts

node_modules: pnpm-lock.yaml
	pnpm install --frozen-lockfile --prefer-offline

prepare: dist dist-cjs dist-esm dist-icons dist-themes ## Build and package for distribution

storybook: node_modules storybook/node_modules dist-icons ## Start local Storybook for component development
	@cd storybook && pnpm exec storybook dev -p 6006

storybook/storybook-static: node_modules storybook/node_modules dist-icons ## Build Storybook for deployment
	@cd storybook && pnpm exec storybook build

storybook/node_modules: storybook/pnpm-lock.yaml
	@cd storybook && pnpm install --frozen-lockfile --prefer-offline

help: # Print a list of available commands (only commands with comments prefixed by ##)
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
