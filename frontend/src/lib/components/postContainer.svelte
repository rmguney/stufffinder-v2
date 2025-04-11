<script>
  import Post from '$lib/components/post.svelte';
  import { threadStore, forceRefreshThreads } from '../../threadStore';
  import { onMount } from 'svelte';
  import Pagination from '$lib/components/ui/pagination/pagination.svelte';

  let currentPage = 1;
  const postsPerPage = 9;

  $: totalPages = Math.ceil($threadStore.length / postsPerPage);
  $: startIndex = (currentPage - 1) * postsPerPage;
  $: endIndex = startIndex + postsPerPage;
  $: displayPosts = $threadStore.slice().sort((a, b) => b.id - a.id).slice(startIndex, endIndex);

  function handlePageChange(newPage) {
    if (newPage >= 1 && newPage <= totalPages) {
      currentPage = newPage;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  onMount(async function () {
    console.log("Thread data sample:", $threadStore.length > 0 ? $threadStore[0] : "No threads");
  });
</script>

<div class="flex flex-col gap-6">
  <div class="flex flex-wrap gap-4 lg:gap-6 justify-center">
    {#each displayPosts as thread}
      <div class="w-full lg:w-[calc(33.333%-1rem)]">
        <a href={`/thread/${thread.id}`}>
          <Post
            id={thread.id}
            title={thread.title}
            description={thread.description || ""}
            tags={thread.tags || []}
            imageSrc={thread.mysteryObjectImageUrl ? thread.mysteryObjectImageUrl : ''}
            mediaFiles={thread.mediaFiles || []}
            postedBy={thread.author}
            createdAt={thread.createdAt}
            updatedAt={thread.updatedAt}
            upvotes={thread.upvotesCount || 0}
            downvotes={thread.downvotesCount || 0}
            commentCount={thread.commentCount || 0}
            userUpvoted={thread.userUpvoted || false}
            userDownvoted={thread.userDownvoted || false}
            solved={thread.solved}
            mysteryObject={thread.mysteryObject || null}
            variant="thumb"
          />
        </a>
      </div>
    {:else}
      <div class="w-full text-center py-8">
        <p>No posts found. Check back later!</p>
      </div>
    {/each}
  </div>

  {#if totalPages > 1}
    <div class="w-full flex justify-center">
      <Pagination 
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={handlePageChange}
      />
    </div>
  {/if}
</div>