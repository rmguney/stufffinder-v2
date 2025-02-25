<script>
  import * as Card from "$lib/components/ui/card";
  import { activeUser } from "../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { threadStore } from "../../threadStore";
  import { Textarea } from "$lib/components/ui/textarea";
  import { Separator } from "$lib/components/ui/separator";

  // Import Comment for recursion
  import Comment from "./comment.svelte";

  export let commentId;
  export let comment;
  export let commentator;
  export let postedDateComment;
  export let selected; // Current "selected" status of the comment
  export let threadOwner; // Owner of the thread
  export let replies = []; // Nested replies for this comment

  let currentUser = null;
  let replyInputVisible = false;
  let replyText = "";

  // Subscribe to the active user
  $: activeUser.subscribe((value) => {
    currentUser = value;
  });

  // Toggle reply input visibility
  const toggleReplyInput = () => {
    replyInputVisible = !replyInputVisible;
  };

  // Add a reply to this comment using the backend API
  const addReply = async () => {
    if (!replyText.trim()) return;

    const payload = {
        content: replyText,
        postId: thread.id,
        parentCommentId: commentId
    };

    try {
        const response = await fetch('http://localhost:8080/api/comments/create', {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            console.error("Failed to add reply");
            return;
        }

        const newReply = await response.json();
        
        // Update local store
        threadStore.update(threads =>
            threads.map(thread => ({
                ...thread,
                comments: thread.comments.map(c =>
                    c.id === commentId
                        ? {
                            ...c,
                            replies: [...(c.replies || []), {
                                id: newReply.id,
                                content: newReply.content,
                                author: newReply.author,
                                createdAt: newReply.createdAt,
                                upvotes: newReply.upvotes,
                                downvotes: newReply.downvotes,
                                bestAnswer: newReply.bestAnswer
                            }]
                        }
                        : c
                )
            }))
        );

        replyText = "";
        replyInputVisible = false;
    } catch (error) {
        console.error("Error adding reply:", error);
    }
  };

  // Format the date to a human-readable format
  const formatDate = (isoDate) => {
  if (!isoDate) return "";
  const date = new Date(isoDate);

  // Extract time components
  const timeOptions = { hour: "numeric", minute: "numeric", hour12: false };
  const timeString = new Intl.DateTimeFormat("en-US", timeOptions).format(date);

  // Extract date components with day before month and full year
  const day = date.getDate();
  const shortMonth = date.toLocaleString("en-US", { month: "short" }); // Shortened month
  const fullYear = date.getFullYear(); // Full year

  // Combine components with time first
  return `${timeString}, ${day} ${shortMonth} ${fullYear}`;
};

  // Function to toggle the helpful status
  const toggleHelpful = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/posts/${thread.id}/markBestAnswer/${commentId}`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );

      if (!response.ok) {
        console.error("Failed to toggle helpful status");
        return;
      }

      const data = await response.json();
      selected = data.bestAnswer; // Update the local "selected" state
    } catch (error) {
      console.error("Error toggling helpful status:", error);
    }
  };
</script>

<div class="flex w-full py-1">
  <Card.Root class={`w-full bg-opacity-90 hover:bg-opacity-100 relative ${selected ? 'border-4 border-teal-600 dark:border-teal-800' : ''}`}>
    <div class="flex flex-col w-full">
      <Card.Header>
        <Card.Title class="w-full flex items-center">
          {comment}
          {#if selected}
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="ml-2 size-5 text-teal-800">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6.633 10.25c.806 0 1.533-.446 2.031-1.08a9.041 9.041 0 0 1 2.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 0 0 .322-1.672V2.75a.75.75 0 0 1 .75-.75 2.25 2.25 0 0 1 2.25 2.25c0 1.152-.26 2.243-.723 3.218-.266.558.107 1.282.725 1.282m0 0h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 0 1-2.649 7.521c-.388.482-.987.729-1.605.729H13.48c-.483 0-.964-.078-1.423-.23l-3.114-1.04a4.501 4.501 0 0 0-1.423-.23H5.904m10.598-9.75H14.25M5.904 18.5c.083.205.173.405.27.602.197.4-.078.898-.523.898h-.908c-.889 0-1.713-.518-1.972-1.368a12 12 0 0 1-.521-3.507c0-1.553.295-3.036.831-4.398C3.387 9.953 4.167 9.5 5 9.5h1.053c.472 0 .745.556.5.96a8.958 8.958 0 0 0-1.302 4.665c0 1.194.232 2.333.654 3.375Z" />
            </svg>
          {/if}
        </Card.Title>
      </Card.Header>

      <Card.Description class="flex flex-col w-full px-6">
        <span>
          <a href={`/user/${commentator}`} class="hover:text-rose-900 hover:underline font-bold">
            {commentator}
          </a>
          at {formatDate(postedDateComment)}
        </span>
        <Separator class="mt-4"/>
        <div class="flex flex-row justify-center mt-4 gap-2">
          {#if currentUser === threadOwner}
            <Button 
              on:click={toggleHelpful} 
              class="w-1/4 text-xs py-1 px-2 hover:bg-rose-900">
              {selected ? "Unmark" : "Mark as Helpful"}
            </Button>
          {/if}

          <Button on:click={toggleReplyInput} class="w-1/4 text-xs py-1 px-2 hover:bg-rose-900">
            {replyInputVisible ? "Cancel" : "Reply"}
          </Button>
          
          {#if replyInputVisible}
          <Button on:click={addReply} class="w-1/4 text-xs py-1 px-2 hover:bg-rose-900">
            Submit
          </Button>
          {/if}
        </div>
        {#if replyInputVisible}
          <div class="mt-2">
            <Textarea 
              bind:value={replyText} 
              class="w-full p-2 border rounded-lg text-sm"
              placeholder="Write your reply..."></Textarea>
          </div>
        {/if}

        <!-- Render Nested Replies -->
        <div class="mt-4 px-4 border-l">
          {#each replies as reply}
            <Comment 
              commentId={reply.id}
              comment={reply.comment}
              commentator={reply.commentator}
              postedDateComment={reply.postedDateComment}
              selected={reply.selected}
              threadOwner={threadOwner}
              replies={reply.replies || []}
            />
          {/each}
        </div>
      </Card.Description>
    </div>
  </Card.Root>
</div>
