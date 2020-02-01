package edu.coursera.distributed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.InputStream;
import java.util.StringJoiner;

/**
 * A basic and very limited implementation of a file server that responds to GET
 * requests from HTTP clients.
 */
public final class FileServer {
    /**
     * Main entrypoint for the basic file server.
     *
     * @param socket Provided socket to accept connections on.
     * @param fs     A proxy filesystem to serve files from. See the PCDPFilesystem
     *               class for more detailed documentation of its usage.
     * @throws IOException If an I/O error is detected on the server. This
     *                     should be a fatal error, your file server
     *                     implementation is not expected to ever throw
     *                     IOExceptions during normal operation.
     */
    public void run(final ServerSocket socket, final PCDPFilesystem fs) throws IOException {
        /*
         * Enter a spin loop for handling client requests to the provided
         * ServerSocket object.
         */
        while (true) {
            // TODO 1) Use socket.accept to get a Socket object
            Socket client = socket.accept();

            /*
             * TODO 2) Using Socket.getInputStream(), parse the received HTTP
             * packet. In particular, we are interested in confirming this
             * message is a GET and parsing out the path to the file we are
             * GETing. Recall that for GET HTTP packets, the first line of the
             * received packet will look something like:
             *
             *     GET /path/to/file HTTP/1.1
             */
            String path = parseRequestPath(client.getInputStream());
            String content = fs.readFile(new PCDPPath(path));

            /*
             * TODO 3) Using the parsed path to the target file, construct an
             * HTTP reply and write it to Socket.getOutputStream(). If the file
             * exists, the HTTP reply should be formatted as follows:
             *
             *   HTTP/1.0 200 OK\r\n
             *   Server: FileServer\r\n
             *   \r\n
             *   FILE CONTENTS HERE\r\n
             *
             * If the specified file does not exist, you should return a reply
             * with an error code 404 Not Found. This reply should be formatted
             * as:
             *
             *   HTTP/1.0 404 Not Found\r\n
             *   Server: FileServer\r\n
             *   \r\n
             *
             * Don't forget to close the output stream.
             */
            final OutputStream os = client.getOutputStream();
            final String response;
            if (content != null) {
                response = createResponse(200, "OK", content);
            } else {
                response = createResponse(404, "Not Found", null);
            }
            os.write(response.getBytes());
            os.close();
        }
    }

    private static String createResponse(final int code, final String message, final String content) {
        final StringJoiner joiner = new StringJoiner("\r\n", "", "\r\n");
        joiner.add(String.format("HTTP/1.0 %d %s", code, message));
        joiner.add("Server: FileServer");
        joiner.add("");
        if (content != null) {
            joiner.add(content);
        }
        return joiner.toString();
    }

    private static String parseRequestPath(final InputStream is) throws IOException {
        final String request = new BufferedReader(new InputStreamReader(is)).readLine();
        final String[] components = request.split(" ");
        if (components.length < 2) {
            throw new IllegalArgumentException("Illegal HTTP request: " + request);
        }
        return components[1];
    }
}
