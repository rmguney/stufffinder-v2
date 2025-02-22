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

      const userResponse = await fetch(`http://localhost:8080/api/auth/${username}`);
      if (!userResponse.ok) throw new Error('User not found');
      const userData = await userResponse.json();
      const userId = userData.id;

      // Fetch user's posts
      const postsResponse = await fetch(`http://localhost:8080/api/auth/${userId}/posts`);
      if (!postsResponse.ok) throw new Error('Failed to fetch posts');
      const postsData = await postsResponse.json();
      threadStore.set(postsData);
      threads = postsData;

      // Fetch user's comments
      const commentsResponse = await fetch(`http://localhost:8080/api/auth/${userId}/comments`);
      if (!commentsResponse.ok) throw new Error('Failed to fetch comments');
      const commentsData = await commentsResponse.json();
      comments = commentsData;

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
