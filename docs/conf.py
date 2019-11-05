# Configuration file for the Sphinx documentation builder.

# -- Project information -----------------------------------------------------
project = 'yaks-java'
copyright = '2019, ATOLabs'
author = 'ATOLabs'
release = '0.3.0'

# -- General configuration ---------------------------------------------------
master_doc = 'index'
extensions = ['javasphinx']
language = 'java'

# -- Options for HTML output -------------------------------------------------
html_theme = 'sphinx_rtd_theme'


def run_javasphinx_apidoc(_):
    from javasphinx import apidoc
    apidoc.main([
        "javasphinx-apidoc", 
        "-f", 
        "-t", "API Refrence",
        "-o", ".",
        "../yaks/src/main/java/"])

def setup(app):
    app.connect('builder-inited', run_javasphinx_apidoc)
