
package com.example.jerrydemo.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RefreshController {
    @Autowired
    private DiscoveryClient discoveryClient;

    private RestTemplate rest = new RestTemplate();

    @GetMapping("/refresh")
    public Map<String, String> refresh() {

        Map<String, String> result = new HashMap<>();
        List<String> services = discoveryClient.getServices();
        result.put("Basic Info", "Total services in k8s:" + services.size());

        services.stream()
                .filter(s -> (!"config-server-k8s".equals(s)) && s.startsWith("config-client"))
                .forEach(service -> {
                    List<ServiceInstance> instances = discoveryClient.getInstances(service);

                    instances.forEach(instance -> {
                        String url = "http://" + instance.getHost() + ":" + instance.getPort() + "/actuator/refresh";

                        try {
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            HttpEntity<String> entity = new HttpEntity<>(null, headers);
                            ResponseEntity<String> response = rest.postForEntity(url, entity, String.class);
                            result.put(service + " " + url, response.getStatusCode().getReasonPhrase());
                        } catch (Exception e) {
                            result.put(service + " " + url, e.getMessage());
                        }
                    });
                });

        return result;
    }
}