<script>
  import Post from '$lib/components/post.svelte';
  import Comment from '$lib/components/comment.svelte';
  import { threadStore } from '../../../threadStore';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';

  let username;
  let threads = [];
  let comments = [];
  let loadingThreads = true;
  let loadingComments = true;

  $: username = $page.params.username;

  // Fetch threads and comments by username
  onMount(async () => {
    try {
      loadingThreads = true;
      loadingComments = true;

      // Fetch all threads
      const threadResponse = await fetch(`https://threef.vercel.app/api/thread`);
      if (!threadResponse.ok) {
        throw new Error(`HTTP error! status: ${threadResponse.status}`);
      }
      const allThreads = await threadResponse.json();
      threadStore.set(allThreads); // Update threadStore
      threads = allThreads.filter(thread => thread.postedBy === username);

      // Fetch all comments
      const commentResponse = await fetch(`https://threef.vercel.app/api/comment`);
      if (!commentResponse.ok) {
        throw new Error(`HTTP error! status: ${commentResponse.status}`);
      }
      const allComments = await commentResponse.json();
      
      // Get user's direct comments and extract their replies from other comments
      const directComments = allComments.filter(comment => comment.commentator === username);
      const repliesFromOthersComments = allComments
        .flatMap(comment => comment.replies || [])
        .filter(reply => reply.commentator === username);
      
      comments = [...directComments, ...repliesFromOthersComments];

    } catch (error) {
      console.error("Error fetching data:", error);
    } finally {
      loadingThreads = false;
      loadingComments = false;
    }
  });
</script>

<div class="flex justify-center p-4 pb-0 lg:py-8 bg-change dark:bg-dark shifting">
  <div class="w-full lg:w-2/3">
    <!-- Threads Section -->
    <div class="flex flex-col items-center mb-8">
      {#if loadingThreads ?? loadingComments}
        <p class="font-bold text-lg">Loading the profile for {username}...</p>
      {/if}

      {#if !loadingThreads && threads.length > 0}
        <div
          class="{threads.length === 1 
            ? 'flex flex-col items-center gap-4' 
            : 'grid gap-4 lg:gap-6 w-full'}"
          style="{threads.length === 1 
            ? '' 
            : 'grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));'}"
        >
          {#each threads as thread}
            <a href={`/thread/${thread.id}`} class="w-full">
              <Post
                id={thread.id}
                title={thread.title}
                description={thread.description}
                tags={thread.tags}
                imageSrc={thread.imageSrc}
                postedBy={thread.postedBy}
                postedDate={thread.postedDate}
                variant="thumb"
                resolved={thread.resolved}
              />
            </a>
          {/each}
        </div>
      {/if}

      {#if !loadingThreads && threads.length === 0}
        <p class="font-bold text-lg">No threads found for {username}.</p>
      {/if}
    </div>

    <!-- Comments Section -->
    <div class="flex flex-col items-center">
      {#if !loadingComments && comments.length > 0}
        <div class="flex flex-col w-full">
          <!-- User's direct comments -->
          {#each comments as comment}
            <a href={`/thread/${comment.thread}`} class="block">
              <Comment
                commentId={comment.id}
                comment={comment.comment}
                commentator={comment.commentator}
                postedDateComment={comment.postedDateComment}
                threadOwner={comment.threadOwner}
                selected={comment.selected}
              />
            </a>
          {/each}
        </div>
      {/if}

      {#if !loadingComments && comments.length === 0}
        <p class="font-bold text-lg">No comments or replies found for {username}.</p>
      {/if}
    </div>
  </div>
</div>
