package com.rahul.ticketbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
@Getter
@Setter
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    //Lazy means the data was never loaded from the DB in the first place — it's fetched only when something asks for it.
    //When Jackson calls getShows() during serialization, JPA tries to go fetch it right then —
    //but the DB session is already closed by that point, so the fetch itself fails. That's the actual exception.
    //@JsonIgnore just tells Jackson: "Don't call getShows() at all when converting this to JSON. Skip it.

    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Show> shows = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}