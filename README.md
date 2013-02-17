### Maven SNAPSHOT Repository Proxy Server

This project is a -very- simple proxy server for Maven repositories for use with
applications that don't know how to handle timestamped SNAPSHOT releases.

When the proxy gets a request for:

    /foo/foobar/2.3-SNAPSHOT/foobar-2.3-SNAPSHOT.jar

Then the upstream metadata XML file is inspected, and the latest file is downloaded
and returned to the caller as thou it were a non-timestamped version.

To run simple add a `configuration.json` file in the current directory looking like:

    {
	  "configuration": "http://your/upstream/repository"
    }

