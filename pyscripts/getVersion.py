#!/usr/bin/python2.6

import ssl
import xmlrpc.client
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default="https://geni.renci.org:11443/orca/xmlrpc")
parser.add_option("-c", "--cert", dest="cert",
                  help="PEM file with cert")
parser.add_option("-p", "--private-key", dest="privateKey",
                  help="Private key file (or a PEM file if contains both private key and cert)")
(options, args) = parser.parse_args()

# Create an object to represent our server.
server_url = options.server;

if server_url.startswith('https://'):
    if options.cert == None or options.privateKey == None:
        print ("For using secure (https) transport, you must specify the path to your certificate and private key")
        parser.print_help()
        exit(-1)
    # create secure transport with client cert
    context = ssl.SSLContext()
    context.load_cert_chain(options.cert, options.privateKey)
    server = xmlrpc.client.ServerProxy(server_url, context=context)
else:
    server = xmlrpc.client.ServerProxy(server_url)

# Call the server and get our result.
print ("Querying ORCA at %s for current AM API version ... \n" % (server_url))
result = server.orca.getVersion()
print ("Current API version = %r" % (result))
