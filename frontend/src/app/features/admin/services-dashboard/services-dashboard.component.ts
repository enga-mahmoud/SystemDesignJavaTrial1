import { Component } from '@angular/core';

interface ServiceCard {
  name: string;
  description: string;
  icon: string;
  accent: string;
  url: string;
  port: string;
  hasUi: boolean;
}

@Component({
  selector: 'app-services-dashboard',
  templateUrl: './services-dashboard.component.html'
})
export class ServicesDashboardComponent {

  platform: ServiceCard[] = [
    {
      name: 'API Gateway',
      description: 'Single entry point for all client traffic. Handles JWT auth, rate limiting, circuit breakers, and service routing.',
      icon: 'fa-door-open',
      accent: '#6366f1',
      url: 'http://localhost:8080/actuator',
      port: '8080',
      hasUi: true
    },
    {
      name: 'Auth Service',
      description: 'User registration, login, JWT issuance, and token refresh. Manages roles: USER, ADMIN, VENDOR.',
      icon: 'fa-shield-halved',
      accent: '#0ea5e9',
      url: 'http://localhost:8081/actuator',
      port: '8081',
      hasUi: true
    },
    {
      name: 'Service Registry',
      description: 'Eureka server — tracks every running microservice instance so the gateway can resolve them by name.',
      icon: 'fa-network-wired',
      accent: '#8b5cf6',
      url: 'http://localhost:8761',
      port: '8761',
      hasUi: true
    },
    {
      name: 'Config Server',
      description: 'Spring Cloud Config — serves centralised YAML configuration to all microservices at startup.',
      icon: 'fa-sliders',
      accent: '#7c3aed',
      url: 'http://localhost:8888/actuator',
      port: '8888',
      hasUi: true
    }
  ];

  observability: ServiceCard[] = [
    {
      name: 'Zipkin',
      description: 'Distributed tracing UI. Search traces by service, endpoint, or correlation ID to diagnose latency.',
      icon: 'fa-binoculars',
      accent: '#f59e0b',
      url: 'http://localhost:9411',
      port: '9411',
      hasUi: true
    },
    {
      name: 'Grafana',
      description: 'Metrics dashboards powered by Prometheus. Monitor JVM heap, HTTP throughput, error rates, and more.',
      icon: 'fa-chart-line',
      accent: '#f97316',
      url: 'http://localhost:3001',
      port: '3001',
      hasUi: true
    },
    {
      name: 'Prometheus',
      description: 'Time-series metrics store. Scrapes /actuator/prometheus from each service every 15 seconds.',
      icon: 'fa-fire-flame-curved',
      accent: '#ef4444',
      url: 'http://localhost:9091',
      port: '9091',
      hasUi: true
    }
  ];

  data: ServiceCard[] = [
    {
      name: 'Elasticsearch',
      description: 'Product search index. Receives events via Kafka and powers the /api/search endpoint.',
      icon: 'fa-magnifying-glass-chart',
      accent: '#10b981',
      url: 'http://localhost:9200/_cat/indices?v',
      port: '9200',
      hasUi: true
    },
    {
      name: 'Redis',
      description: 'In-memory cache for product responses, rate-limit sliding windows, and refresh token storage.',
      icon: 'fa-bolt',
      accent: '#dc2626',
      url: '',
      port: '6379',
      hasUi: false
    },
    {
      name: 'Kafka',
      description: 'Message broker for async event streaming: outbox relay, order saga, inventory, notifications.',
      icon: 'fa-right-left',
      accent: '#7c3aed',
      url: '',
      port: '9094',
      hasUi: false
    }
  ];

  open(url: string): void {
    window.open(url, '_blank', 'noopener');
  }
}
