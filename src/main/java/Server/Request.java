package Server;

import java.util.List;

public class Request {
    private final String method;
    private final List<String> headers;
    private final String body;

    private final String path;

    public Request(String method, List<String> headers, String body, String path) {
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", headers=" + headers +
                ", body='" + body + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
