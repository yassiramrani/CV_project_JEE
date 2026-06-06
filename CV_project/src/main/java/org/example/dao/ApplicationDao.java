package org.example.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Application;

import java.util.List;

@Stateless
public class ApplicationDao {

    @PersistenceContext(unitName = "cvPU")
    private EntityManager em;

    public void save(Application application) {
        em.persist(application);
    }

    public Application findById(Long id) {
        return em.find(Application.class, id);
    }

    public List<Application> findByCandidateId(Long candidateId) {
        return em.createQuery("SELECT a FROM Application a WHERE a.candidate.id = :candidateId ORDER BY a.applicationDate DESC", Application.class)
                .setParameter("candidateId", candidateId)
                .getResultList();
    }

    public List<Application> findByJobOfferId(Long jobOfferId) {
        return em.createQuery("SELECT a FROM Application a WHERE a.jobOffer.id = :jobOfferId ORDER BY a.score DESC", Application.class)
                .setParameter("jobOfferId", jobOfferId)
                .getResultList();
    }

    public void update(Application application) {
        em.merge(application);
    }

    public void delete(Long id) {
        Application application = findById(id);
        if (application != null) {
            em.remove(application);
        }
    }
}
