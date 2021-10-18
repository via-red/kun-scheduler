package com.miotech.kun.metadata.databuilder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miotech.kun.metadata.core.model.constant.DatabaseType;
import com.miotech.kun.metadata.databuilder.extract.tool.ConnectUrlUtil;

public class PostgresDataSource extends DataSource {

    private final String host;

    private final int port;

    private final String username;

    private final String password;

    public PostgresDataSource(@JsonProperty("id") long id,
                              @JsonProperty("host") String host,
                              @JsonProperty("port") int port,
                              @JsonProperty("username") String username,
                              @JsonProperty("password") String password) {
        super(id, Type.POSTGRESQL);
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return ConnectUrlUtil.convertToConnectUrl(host, port, username, password, DatabaseType.POSTGRES);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String host;
        private int port;
        private String username;
        private String password;
        private long id;

        private Builder() {
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withId(long id) {
            this.id = id;
            return this;
        }

        public PostgresDataSource build() {
            return new PostgresDataSource(id, host, port, username, password);
        }
    }
}
