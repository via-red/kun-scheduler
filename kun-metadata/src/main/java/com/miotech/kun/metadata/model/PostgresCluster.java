package com.miotech.kun.metadata.model;

public class PostgresCluster extends Cluster {

    private final String url;

    private final String username;

    private final String password;

    public PostgresCluster(long clusterId, String url, String username, String password) {
        super(clusterId);
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "PostgresCluster{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private long clusterId;
        private String url;
        private String username;
        private String password;

        private Builder() {
        }

        public Builder withClusterId(long clusterId) {
            this.clusterId = clusterId;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
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

        public PostgresCluster build() {
            return new PostgresCluster(clusterId, url, username, password);
        }
    }
}
