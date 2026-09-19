package com.example.bandlink.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="conversations", uniqueConstraints=@UniqueConstraint(name="uk_conversation_pair",columnNames={"user_a_id","user_b_id"}))
public class Conversation {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_a_id") private User userA;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_b_id") private User userB;
  @Column(nullable=false) private LocalDateTime createdAt;
  private LocalDateTime lastMessageAt;
  // A one-sided "delete conversation": hiding it only ever sets this user's own timestamp, never
  // touches the other side's copy or any message, so the other participant sees nothing different
  // (requirements: delete my own copy without the other person noticing). isHiddenFor compares
  // against lastMessageAt rather than clearing the timestamp on the next message, so a hidden
  // conversation reappears on its own the moment new activity happens in it - no separate
  // "unhide" call needed anywhere senders touch the conversation.
  // Named explicitly: the default naming strategy collapses the adjacent capitals in
  // hiddenForAAt/hiddenForBAt into "hidden_foraat"/"hidden_forbat", which reads like a typo.
  @Column(name="hidden_for_a_at") private LocalDateTime hiddenForAAt;
  @Column(name="hidden_for_b_at") private LocalDateTime hiddenForBAt;
  protected Conversation(){}
  public Conversation(User a,User b,LocalDateTime now){if(a.getId()!=null&&b.getId()!=null&&a.getId()>b.getId()){var t=a;a=b;b=t;}userA=a;userB=b;createdAt=now;lastMessageAt=now;}
  public Long getId(){return id;}
  public User getUserA(){return userA;}
  public User getUserB(){return userB;}
  public LocalDateTime getLastMessageAt(){return lastMessageAt;}
  public void touch(LocalDateTime now){lastMessageAt=now;}
  public boolean includes(Long userId){return userA.getId().equals(userId)||userB.getId().equals(userId);}
  public void hideFor(Long userId,LocalDateTime now){if(userA.getId().equals(userId))hiddenForAAt=now;else if(userB.getId().equals(userId))hiddenForBAt=now;}
  public boolean isHiddenFor(Long userId){
    LocalDateTime hiddenAt=userA.getId().equals(userId)?hiddenForAAt:userB.getId().equals(userId)?hiddenForBAt:null;
    return hiddenAt!=null && !hiddenAt.isBefore(lastMessageAt);
  }
}
