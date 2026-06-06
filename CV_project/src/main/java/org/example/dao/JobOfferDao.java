package org.example.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.JobOffer;

import java.util.List;

@Stateless
public class JobOfferDao {

    @PersistenceContext(unitName = "cvPU")
    private EntityManager em;

    public void save(JobOffer jobOffer) {
        em.persist(jobOffer);
    }

    public JobOffer findById(Long id) {
        return em.find(JobOffer.class, id);
    }

    public List<JobOffer> findAll() {
        return em.createQuery("SELECT j FROM JobOffer j ORDER BY j.createdAt DESC", JobOffer.class).getResultList();
    }

    public List<JobOffer> findByRecruiterId(Long recruiterId) {
        return em.createQuery("SELECT j FROM JobOffer j WHERE j.recruiter.id = :recruiterId ORDER BY j.createdAt DESC", JobOffer.class)
                .setParameter("recruiterId", recruiterId)
                .getResultList();
    }

    public void update(JobOffer jobOffer) {
        em.merge(jobOffer);
    }

    public void delete(Long id) {
        JobOffer jobOffer = findById(id);
        if (jobOffer != null) {
            em.remove(jobOffer);
        }
    }
}
