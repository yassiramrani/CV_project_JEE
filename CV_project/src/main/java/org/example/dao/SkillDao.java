package org.example.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.model.Skill;

import java.util.List;

@Stateless
public class SkillDao {

    @PersistenceContext(unitName = "cvPU")
    private EntityManager em;

    public void save(Skill skill) {
        em.persist(skill);
    }

    public Skill findById(Long id) {
        return em.find(Skill.class, id);
    }

    public List<Skill> findAll() {
        return em.createQuery("SELECT s FROM Skill s ORDER BY s.name ASC", Skill.class).getResultList();
    }

    public Skill findByName(String name) {
        var query = em.createQuery("SELECT s FROM Skill s WHERE s.name = :name", Skill.class);
        query.setParameter("name", name);
        var results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public void delete(Long id) {
        Skill skill = findById(id);
        if (skill != null) {
            em.remove(skill);
        }
    }
}
