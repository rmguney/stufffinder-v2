<script>
  import { Button } from "$lib/components/ui/button";
  import { ChevronLeft, ChevronRight } from "lucide-svelte";

  export let currentPage = 1;
  export let totalPages = 1;
  export let onPageChange;

  function handlePageChange(page) {
    if (page >= 1 && page <= totalPages) {
      onPageChange(page);
    }
  }

  function getPageNumbers() {
    const pages = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }
</script>

<div class="flex items-center gap-2">
  <Button
    variant="outline"
    size="icon"
    on:click={() => handlePageChange(currentPage - 1)}
    disabled={currentPage === 1}
  >
    <ChevronLeft class="h-4 w-4" />
  </Button>

  {#each getPageNumbers() as page}
    <Button
      variant={page === currentPage ? "default" : "outline"}
      size="icon"
      on:click={() => handlePageChange(page)}
    >
      {page}
    </Button>
  {/each}

  <Button
    variant="outline"
    size="icon"
    on:click={() => handlePageChange(currentPage + 1)}
    disabled={currentPage === totalPages}
  >
    <ChevronRight class="h-4 w-4" />
  </Button>
</div> 