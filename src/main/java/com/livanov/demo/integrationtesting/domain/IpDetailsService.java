package com.livanov.demo.integrationtesting.domain;

import com.livanov.demo.integrationtesting.domain.ports.CachedIpDetailsService;
import com.livanov.demo.integrationtesting.domain.ports.IpDetailsRepository;
import com.livanov.demo.integrationtesting.domain.ports.RemoteIpDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@RequiredArgsConstructor
public class IpDetailsService {

    private final RemoteIpDetailsService remoteService;
    private final IpDetailsRepository repository;
    private final CachedIpDetailsService cachedService;

    public IpDetails getInfo(String ip) {

        Optional<IpDetails> ipInfoFromCache = tryGetFromCache(ip);

        if (ipInfoFromCache.isPresent()) {
            log.info("IpDetails [{}]     retrieved from Cache.", ip);

            return ipInfoFromCache.get();
        }

        Optional<IpDetails> ipInfoFromDb = tryGetFromDb(ip);

        if (ipInfoFromDb.isPresent()) {
            log.info("IpDetails [{}]     retrieved from Database.", ip);

            IpDetails ipDetails = ipInfoFromDb.get();
            trySaveToCache(ipDetails);

            return ipDetails;
        }

        Optional<IpDetails> ipInfoFromService = tryGetFromThirdParty(ip);

        if (ipInfoFromService.isPresent()) {
            log.info("IpDetails [{}]     retrieved from Third Party Provider Service.", ip);

            IpDetails ipDetails = ipInfoFromService.get();
            trySaveToDb(ipDetails);
            trySaveToCache(ipDetails);

            return ipDetails;
        }

        throw new IpDetailsNotFoundException(ip);
    }

    public List<IpDetails> getAll() {

        Spliterator<IpDetails> all = repository.findAll().spliterator();

        return StreamSupport.stream(all, false)
                .collect(toList());
    }

    private Optional<IpDetails> tryGetFromCache(String ip) {
        try {
            return cachedService.getInfo(ip);
        } catch (Exception ex) {
            log.warn("IpDetails [" + ip + "] NOT retrieved from Cache.");
            return Optional.empty();
        }
    }

    private void trySaveToCache(IpDetails ipDetails) {
        try {
            cachedService.cache(ipDetails);
            log.debug("IpDetails [{}]     saved to Cache.", ipDetails.getIp());
        } catch (Exception ex) {
            log.warn("IpDetails [" + ipDetails.getIp() + "] NOT saved to Cache.");
        }
    }

    private Optional<IpDetails> tryGetFromDb(String ip) {
        try {
            return repository.findOneByIp(ip);
        } catch (Exception ex) {
            log.warn("IpDetails [" + ip + "] NOT retrieved from Database.");
            return Optional.empty();
        }
    }

    private void trySaveToDb(IpDetails ipDetails) {
        try {
            repository.save(ipDetails);
            log.debug("IpDetails [{}]     saved to Database.", ipDetails.getIp());
        } catch (Exception ex) {
            log.warn("IpDetails [" + ipDetails.getIp() + "] NOT saved to Database.");
        }
    }

    private Optional<IpDetails> tryGetFromThirdParty(String ip) {
        try {
            return remoteService.getInfo(ip);
        } catch (Exception ex) {
            log.warn("IpDetails [" + ip + "] NOT retrieved from Third Party Provider.");
            return Optional.empty();
        }
    }
}
