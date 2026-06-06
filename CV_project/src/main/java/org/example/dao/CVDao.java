package org.example.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.CV;

@Stateless
public class CVDao {

    @PersistenceContext(unitName = "cvPU")
    private EntityManager em;

    public void save(CV cv) {
        em.persist(cv);
    }

    public CV findById(Long id) {
        return em.find(CV.class, id);
    }

    public CV findByCandidateId(Long candidateId) {
        var query = em.createQuery("SELECT c FROM CV c WHERE c.candidate.id = :candidateId", CV.class);
        query.setParameter("candidateId", candidateId);
        var results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public void delete(Long id) {
        CV cv = findById(id);
        if (cv != null) {
            em.remove(cv);
        }
    }
}
