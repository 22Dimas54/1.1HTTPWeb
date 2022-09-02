package Server;

import org.apache.http.NameValuePair;

import java.util.List;
import java.util.stream.Collectors;

public class Request {
    private final String method;
    private final List<String> headers;
    private final String body;
    private final String path;
    private final List<NameValuePair> queryParams;

    public Request(String method, List<String> headers, String body, String path, List<NameValuePair> queryParams) {
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.path = path;
        this.queryParams = queryParams;
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

    public List<NameValuePair> getQueryParam(String name) {
        return queryParams.stream()
                .filter(item -> item.getName().equals(name))
                .collect(Collectors.toList());
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }
}
